#include <jni.h>
#include <unistd.h>
#include <termios.h>
#include <fcntl.h>
#include <cstdlib>
#include <wait.h>
#include <android/log.h>
#include <pthread.h>
#include "logging.h"
#include "pts.h"

static int setWindowSize(int ptmx, jlong size) {
    static_assert(sizeof(jlong) == sizeof(winsize));
    winsize w = *((winsize *) &size);

    LOGD("setWindowSize %d %d %d %d", w.ws_row, w.ws_col, w.ws_xpixel, w.ws_ypixel);

    if (ioctl(ptmx, TIOCSWINSZ, &w) == -1) {
        PLOGE("ioctl TIOCGWINSZ");
        return -1;
    }
    return 0;
}

// libcore/ojluni/src/main/native/UNIXProcess_md.c

static void *xmalloc(JNIEnv *env, size_t size) {
    void *p = malloc(size);
    if (p == nullptr)
        env->ThrowNew(env->FindClass("java/lang/OutOfMemoryError"), nullptr);
    else
        memset(p, 0, size);
    return p;
}

#define NEW(type, n) ((type *) xmalloc(env, (n) * sizeof(type)))

static const char *getBytes(JNIEnv *env, jbyteArray arr) {
    return arr == nullptr ? nullptr : (const char *) env->GetByteArrayElements(arr, nullptr);
}

static void releaseBytes(JNIEnv *env, jbyteArray arr, const char *parr) {
    if (parr != nullptr)
        env->ReleaseByteArrayElements(arr, (jbyte *) parr, JNI_ABORT);
}

static void initVectorFromBlock(const char **vector, const char *block, int count) {
    int i;
    const char *p;
    for (i = 0, p = block; i < count; i++) {
        /* Invariant: p always points to the start of a C string. */
        vector[i] = p;
        while (*(p++));
    }
    vector[count] = nullptr;
}

static jintArray RishHost_startHost(
        JNIEnv *env, jclass clazz,
        jbyteArray argBlock, jint argc,
        jbyteArray envBlock, jint envc,
        jbyteArray dirBlock,
        jbyte tty,
        jint stdin_read, jint stdout_write, jint stderr_write) {

    bool in_tty = tty & ATTY_IN;
    bool out_tty = tty & ATTY_OUT;
    bool err_tty = tty & ATTY_ERR;

    int ptmx = -1;
    if (tty) {
        ptmx = open_ptmx();
        if (ptmx == -1) {
            env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), "Unable to open ptmx");
            return nullptr;
        }
        LOGD("ptmx %d", ptmx);
    }

    int stdin_pipe[2]{-1}, stdout_pipe[2]{-1}, stderr_pipe[2]{-1};

    LOGD("istty stdin %d stdout %d stderr %d", (tty & ATTY_IN) ? 1 : 0, (tty & ATTY_OUT) ? 1 : 0, (tty & ATTY_ERR) ? 1 : 0);

    if (!in_tty) {
        pipe2(stdin_pipe, 0);
    }
    if (!out_tty) {
        pipe2(stdout_pipe, 0);
    }
    if (!err_tty) {
        pipe2(stderr_pipe, 0);
    }

    const char *pargBlock = getBytes(env, argBlock);
    const char **argv = NEW(const char *, argc + 2);
    argv[0] = "/system/bin/sh";
    initVectorFromBlock(argv + 1, pargBlock, argc);

    for (int i = 0; i < argc + 2; ++i) {
        LOGD("arg%d=%s", i, argv[i]);
    }

    const char **envv = nullptr;
    const char *penvBlock = nullptr;
    if (envc > 0) {
        penvBlock = getBytes(env, envBlock);
        envv = NEW(const char *, envc + 1);

        initVectorFromBlock(envv, penvBlock, envc);
    }

    const char *pdir = nullptr;
    if (dirBlock) {
        pdir = getBytes(env, dirBlock);
    }

    auto pid = fork();
    if (pid == -1) {
        releaseBytes(env, argBlock, pargBlock);
        releaseBytes(env, envBlock, penvBlock);
        releaseBytes(env, dirBlock, pdir);

        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), "Unable to fork");
        return nullptr;
    }

    if (pid > 0) {
        releaseBytes(env, argBlock, pargBlock);
        releaseBytes(env, envBlock, penvBlock);
        releaseBytes(env, dirBlock, pdir);

        auto called = std::make_shared<std::atomic_bool>(false);
        auto func = [pid, called]() {
            if (called->exchange(true)) {
                return;
            }

            LOGW("client dead, kill forked process");
            kill(pid, SIGKILL);
        };

        if (in_tty) {
            transfer_async(stdin_read, ptmx/*, func*/);
        } else {
            transfer_async(stdin_read, stdin_pipe[1]/*, func*/);
            close(stdin_pipe[0]);
        }

        if (out_tty) {
            transfer_async(ptmx, stdout_write, func);
        } else {
            transfer_async(stdout_pipe[0], stdout_write, func);
            close(stdout_pipe[1]);
        }

        if (!err_tty) {
            transfer_async(stderr_pipe[0], stderr_write/*, func*/);
            close(stderr_pipe[1]);
        }

        auto result = env->NewIntArray(2);
        env->SetIntArrayRegion(result, 0, 1, &pid);
        env->SetIntArrayRegion(result, 1, 1, &ptmx);
        return result;
    } else {
        if (setsid() < 0) {
            PLOGE("setsid");
            exit(1);
        }

        if (pdir) {
            LOGD("attempt to chdir %s", pdir);

            if (access(pdir, X_OK) == 0) {
                if (chdir(pdir) == -1) {
                    PLOGE("chdir %s", pdir);
                } else {
                    LOGD("chdir %s", pdir);
                }
            } else {
                PLOGE("access %s", pdir);
            }
        }

        int pts = -1;
        if (tty) {
            char pts_slave[PATH_MAX]{0};
            if (ptsname_r(ptmx, pts_slave, PATH_MAX - 1) == -1) {
                PLOGE("ptsname_r");
                exit(1);
            }

            if ((pts = open(pts_slave, O_RDWR)) == -1) {
                PLOGE("open %s", pts_slave);
            }
            LOGD("pts %d", pts);
        } else {
            LOGD("no need pts");
        }

        if (in_tty) {
            dup2(pts, STDIN_FILENO);
            LOGD("pts -> in");
        } else {
            dup2(stdin_pipe[0], STDIN_FILENO);
            close(stdin_pipe[1]);
            LOGD("pipe -> in");
        }

        if (out_tty) {
            dup2(pts, STDOUT_FILENO);
            LOGD("pts -> out");
        } else {
            dup2(stdout_pipe[1], STDOUT_FILENO);
            close(stdout_pipe[0]);
            LOGD("pipe -> out");
        }

        if (err_tty) {
            dup2(pts, STDERR_FILENO);
            LOGD("pts -> err");
        } else {
            dup2(stderr_pipe[1], STDERR_FILENO);
            close(stderr_pipe[0]);
            LOGD("pipe -> err");
        }

        LOGD("istty stdin %d stdout %d stderr %d", isatty(STDIN_FILENO), isatty(STDOUT_FILENO), isatty(STDERR_FILENO));

        if (pts != -1) {
            close(pts);
        }

        if (envv) {
            if (execvpe("/system/bin/sh", (char *const *) argv, (char *const *) envv) == -1) {
                PLOGE("execv");
                exit(1);
            }
        } else {
            if (execvp("/system/bin/sh", (char *const *) argv) == -1) {
                PLOGE("execv");
                exit(1);
            }
        }
        exit(0);
    }
}

static void RishHost_setWindowSize(JNIEnv *env, jclass clazz, jint ptmx, jlong size) {
    setWindowSize(ptmx, size);
}

static jint RishHost_waitFor(JNIEnv *env, jclass clazz, jint pid) {
    if (pid < 0)
        return -1;

    int status;
    int w;
    do {
        w = TEMP_FAILURE_RETRY(waitpid(pid, &status, 0));
        if (w == -1) {
            if (errno == ECHILD) {
                return 0;
            }
            PLOGE("waitpid");
            return -1;
        }

        if (WIFEXITED(status)) {
            LOGD("exited with %d", WEXITSTATUS(status));
            return WEXITSTATUS(status);
        } else if (WIFSIGNALED(status)) {
            LOGD("killed by signal %d", WTERMSIG(status));
            return 0;
        }
    } while (!WIFEXITED(status) && !WIFSIGNALED(status));

    return -1;
}

int rikka_rish_RishHost_registerNatives(JNIEnv *env) {
    auto clazz = env->FindClass("rikka/rish/RishHost");
    JNINativeMethod methods[] = {
            {"start",         "([BI[BI[BBIII)[I", (void *) RishHost_startHost},
            {"setWindowSize", "(IJ)V",            (void *) RishHost_setWindowSize},
            {"waitFor",       "(I)I",             (void *) RishHost_waitFor},
    };
    return env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0]));
}
