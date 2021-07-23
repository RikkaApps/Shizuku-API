#ifndef PTS_H
#define PTS_H

#include <functional>

#define ATTY_IN    (1 << 0)
#define ATTY_OUT   (1 << 1)
#define ATTY_ERR   (1 << 2)
#define ATTY_ALL   (ATTY_IN | ATTY_OUT | ATTY_ERR)

int make_tty_raw(int fd, termios &old_termios);

int restore_fd(int fd, const termios &old_termios);

void transfer_async(int in, int out, const std::function<void()> &function = nullptr, bool close_in = true, bool close_out = true);

int open_ptmx();

#endif //PTS_H
