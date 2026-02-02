    .text
    .align  2
    .global z_syscall
    .type   z_syscall, %function
z_syscall:
    push    {r7, lr}         
    mov     r7, r0          
    mov     r0, r1           
    mov     r1, r2          
    mov     r2, r3           
    ldr     r3, [sp, #8]    
    ldr     r4, [sp, #12]    
    ldr     r5, [sp, #16]    
    ldr     r6, [sp, #20]    
    svc     #0             
    pop     {r7, pc}      