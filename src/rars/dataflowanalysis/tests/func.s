double_negatif:
    li   t0, -2                 # t0 = -
    mul  a0, a0, t0             # a0 = + * - = -
    ret                

main:
    li   s0, 5                  # s0 = +
    li   a0, 3                  # a0 = +
    j    double_negatif         
    
    add  t1, a0, s0             # t1 = top + + = top
    add  t2, s0, s0             # t2 = + + + = +  
    
    li   a7, 10
    ecall
