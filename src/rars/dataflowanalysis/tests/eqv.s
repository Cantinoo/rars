.eqv TAILLE   10
.eqv OFFSET   -2
.eqv ZERO_VAL  0
.eqv DOUBLE    2

main:
    li   t0, TAILLE    # t0 = +
    li   t1, OFFSET    # t1 = -
    li   t2, ZERO_VAL  # t2 = 0
    li   t3, DOUBLE    # t3 = +

    mul  t4, t0, t3    # t4 = + * + = + 
    add  t5, t1, t2    # t5 = - + 0 = -
    sub  t6, t0, t4    # t6 = + - + = top 
    mul  a0, t1, t1    # a0 = - * - = +

    li   a7, 10
    ecall
