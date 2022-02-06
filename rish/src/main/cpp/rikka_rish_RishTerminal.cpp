#include <jni.h>
#include <unistd.h>
#include <termios.h>
#include <fcntl.h>
#include <cstdlib>
#include <android/log.h>
#include <pthread.h>
#include "logging.h"
#include "pts.h"

static pthread_mutex_t mutex, winch_mutex;
static struct termios old_stdin{};
static int tty_in_raw = 0;

static int64_t getWindowSize(int fd) {
    static_assert(sizeof(jlong) == sizeof(winsize));
    jlong screen_size;

    if (ioctl(fd, TIOCGWINSZ, &screen_size) == -1) {
        PLOGE("ioctl TIOCGWINSZ");
        return 0;
    }

    return screen_size;
}

static jbyte RishTerminal_prepare(JNIEnv *env, jclass clazz) {
    jbyte atty = 0;
    if (isatty(STDIN_FILENO)) atty |= ATTY_IN;
    if (isatty(STDOUT_FILENO)) atty |= ATTY_OUT;
    if (isatty(STDERR_FILENO)) atty |= ATTY_ERR;

    LOGD("istty stdin %d stdout %d stderr %d", (atty & ATTY_IN) ? 1 : 0, (atty & ATTY_OUT) ? 1 : 0,
         (atty & ATTY_ERR) ? 1 : 0);

    if (atty & ATTY_OUT) {
        sigset_t winch;
        sigemptyset(&winch);
        sigaddset(&winch, SIGWINCH);
        pthread_sigmask(SIG_BLOCK, &winch, nullptr);
    }

    return atty;
}

static jint RishTerminal_start(
        JNIEnv *env, jclass clazz, jbyte tty,
        jint stdin_pipe, jint stdout_pipe, jint stderr_pipe) {

    int tty_fd;
    bool in_tty = tty & ATTY_IN;
    bool out_tty = tty & ATTY_OUT;
    bool err_tty = tty & ATTY_ERR;
    if (in_tty) {
        tty_fd = STDIN_FILENO;
    } else if (out_tty) {
        tty_fd = STDOUT_FILENO;
    } else if (err_tty) {
        tty_fd = STDERR_FILENO;
    } else {
        tty_fd = -1;
    }

    if (tty == ATTY_ALL) {
        if (make_tty_raw(tty_fd, old_stdin) == 0) {
            tty_in_raw = 1;
        }
    }

    if (pthread_mutex_lock(&mutex) != 0) {
        PLOGE("pthread_mutex_lock");
    }

    auto called = std::make_shared<std::atomic_bool>(false);
    auto func = [=]() {
        if (called->exchange(true)) {
            return;
        }

        LOGI("remote exit");

        if (tty_in_raw) {
            if (restore_fd(tty_fd, old_stdin) == 0) {
                tty_in_raw = 0;
            }
        }

        if (pthread_mutex_unlock(&mutex) != 0) {
            PLOGE("pthread_mutex_unlock");
        }
    };

    transfer_async(STDIN_FILENO, stdin_pipe/*, func*/);
    transfer_async(stdout_pipe, STDOUT_FILENO, func);
    if (!err_tty) {
        transfer_async(stderr_pipe, STDERR_FILENO/*, func*/);
    }

    auto sigwinch_handler = [](int sig) {
        if (pthread_mutex_unlock(&winch_mutex) != 0) {
            PLOGE("pthread_mutex_unlock");
        }
    };

    signal(SIGWINCH, sigwinch_handler);

    return tty_fd;
}

static jlong RishTerminal_waitForWindowSizeChange(JNIEnv *env, jclass clazz, jint fd) {
    if (pthread_mutex_lock(&winch_mutex) != 0) {
        PLOGE("pthread_mutex_lock");
    }

    return (jlong) getWindowSize(fd);
}

static void RishTerminal_waitForProcessExit(JNIEnv *env, jclass clazz) {
    if (pthread_mutex_lock(&mutex) != 0) {
        PLOGE("pthread_mutex_lock");
    }

    if (pthread_mutex_unlock(&mutex) != 0) {
        PLOGE("pthread_mutex_unlock");
    }
}

int rikka_rish_RishTerminal_registerNatives(JNIEnv *env) {
    pthread_mutex_init(&mutex, nullptr);
    pthread_mutex_init(&winch_mutex, nullptr);

    auto clazz = env->FindClass("rikka/rish/RishTerminal");
    JNINativeMethod methods[] = {
            {"prepare",                 "()B",     (void *) RishTerminal_prepare},
            {"start",                   "(BIII)I", (void *) RishTerminal_start},
            {"waitForWindowSizeChange", "(I)J",    (void *) RishTerminal_waitForWindowSizeChange},
            {"waitForProcessExit",      "()V",     (void *) RishTerminal_waitForProcessExit},
    };
    return env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0]));
}
