#if 1
#define LIBFFI_ASM
#include <fficonfig.h>
#include <ffi.h>
.text

.extern ___chkstk
.extern _ffi_closure_SYSV_inner

# ffi_closure_OUTER will be called with these registers set:
#    rax points to 'closure'
#    r11 contains a bit mask that specifies which of the
#    first four parameters are float or double
#
# It must move the parameters passed in registers to their stack location,
# call ffi_closure_SYSV_inner for the actual work, then return the result.
# 
.globl _ffi_closure_OUTER	
_ffi_closure_OUTER:     
	# save actual arguments to their stack space.
	test	$1,%r11
	jne	.Lfirst_is_float	
	mov	%rcx, 8(%rsp)
	jmp	.Lsecond
.Lfirst_is_float:
	movlpd	%xmm0, 8(%rsp)

.Lsecond:
	test	$2, %r11
	jne	.Lsecond_is_float	
	mov	%rdx, 16(%rsp)
	jmp	.Lthird
.Lsecond_is_float:
	movlpd	%xmm1, 16(%rsp)

.Lthird:
	test	$4, %r11
	jne	.Lthird_is_float	
	mov	%r8,24(%rsp)
	jmp	.Lfourth
.Lthird_is_float:
	movlpd	%xmm2, 24(%rsp)

.Lfourth:
	test	$8, %r11
	jne	.Lfourth_is_float	
	mov	%r9, 32(%rsp)
	jmp	.Ldone
.Lfourth_is_float:
	movlpd	%xmm3, 32(%rsp)

.Ldone:
#.ALLOCSTACK 40
	sub	$40, %rsp
#.ENDPROLOG
	mov	%rax, %rcx	# context is first parameter
	mov	%rsp, %rdx	# stack is second parameter
	add	$40, %rdx	# correct our own area
	mov	_ffi_closure_SYSV_inner, %rax
	callq	*%rax		# call the real closure function
	## Here, code is missing that handles float return values
	add	$40, %rsp
	movq	%rax, %xmm0	# In case the closure returned a float.
	retq
.ffi_closure_OUTER_end:


#define STACK 0
#define PREPFUNC 32
#define ECIF 40
#define BYTES 48
#define FLAGS 56
#define RVALUE 64
#define FN 72

.globl	_ffi_call_AMD64
	.balign 16
_ffi_call_AMD64:        

	mov	%r9,32(%rsp)
	mov	%r8,24(%rsp)
	mov	%rdx,16(%rsp)
	mov	%rcx,8(%rsp)
#.PUSHREG rbp
	push	%rbp
#.ALLOCSTACK 48
	sub	$48,%rsp
#.SETFRAME rbp, 32
	lea	32(%rsp),%rbp
#.ENDPROLOG

	mov	BYTES(%rbp),%eax
	add	$15, %rax
	and	$-16, %rax
	callq	___chkstk
	sub	%rax, %rsp
	lea	32(%rsp), %rax
	mov	%rax, STACK(%rbp)

	mov	ECIF(%rbp), %rdx
	mov	STACK(%rbp), %rcx
	callq	*PREPFUNC(%rbp)

	mov	STACK(%rbp), %rsp

	movlpd	24(%rsp), %xmm3
	movd	%xmm3, %r9

	movlpd	16(%rsp), %xmm2
	movd	%xmm2, %r8

	movlpd	8(%rsp), %xmm1
	movd	%xmm1, %rdx

	movlpd	(%rsp), %xmm0
	movd	%xmm0, %rcx

	callq	*FN(%rbp)
.Lret_int:
 	cmpl	$FFI_TYPE_INT, FLAGS(%rbp)
 	jne	.Lret_float

	mov	RVALUE(%rbp), %rcx
	mov	%eax, (%rcx)
	jmp	.Lret_nothing

.Lret_float:
 	cmpl	$FFI_TYPE_FLOAT, FLAGS(%rbp)
 	jne	.Lret_double

 	mov	RVALUE(%rbp), %rax
 	movlpd	%xmm0,(%rax)
 	jmp	.Lret_nothing

.Lret_double:
 	cmpl	$FFI_TYPE_DOUBLE, FLAGS(%rbp)
 	jne	.Lret_int64

 	mov	RVALUE(%rbp), %rax
 	movlpd	%xmm0, (%rax)
 	jmp	.Lret_nothing

.Lret_int64:
  	cmpl	$FFI_TYPE_SINT64, (%rbp)
  	jne	.Lret_nothing

 	mov	RVALUE(%rbp), %rcx
 	mov	%rax, (%rcx)
 	jmp	.Lret_nothing
	
.Lret_nothing:
	xor	%eax, %eax

	lea	16(%rbp), %rsp
	pop	%rbp
	retq
.ffi_call_AMD64_end:
#endif