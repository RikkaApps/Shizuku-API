#ifndef PTS_H
#define PTS_H

#include <functional>

int make_fd_raw(int fd, termios &old_termios);

int restore_fd(int fd, const termios &old_termios);

void transfer_async(int in, int out, const std::function<void()> &function = nullptr, bool close_in = true, bool close_out = true);

int open_ptmx();

#endif //PTS_H
