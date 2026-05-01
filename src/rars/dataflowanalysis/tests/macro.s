.macro DOUBLE(%x)
    add %x, %x, %x
.end_macro

.text
main:
    li t0, 5
    DOUBLE(t0)
    DOUBLE(t0)
    li a7, 10
    ecall