# a simple code showing that the + symbol spreads throughout the code
# and that a TOP result is detected when adding a negative number

main:
    li t0, 10           # t0 = +
    addi t0, t0, 5      # t0 = + + + = +
    addi t0, t0, 10     # t0 = + + + = +
    addi t0, t0, 20     # t0 = + + + = +
    addi t0, t0, -15    # t0 = + + - = TOP
    li   a7, 10
    ecall
