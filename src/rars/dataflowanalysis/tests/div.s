main:
        li s0, 2        # s0 = +
        li s1, 2        # s1 = +
        div s0, s0, s1  # s0 = 0+
        li a7,10
        ecall