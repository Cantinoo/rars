main:
    li   t0, -1        # t0 = -  (i = -1)
    li   t1, 5         # t1 = +  (i < 5)
    li   t2, 1         # t2 = +  (i++)

loop:
    bge  t0, t1, end   # for (int i = -1; i <= 5; i++) {
    addi t0, t0, 1     #    // nothing happens here, just incrementing i    
    j loop             # }

end:
    mv   a0, t2
    li   a7, 10
    ecall
