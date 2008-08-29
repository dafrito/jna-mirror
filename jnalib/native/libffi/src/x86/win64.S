#ifdef _MSC_VER
PUBLIC	ffi_call_AMD64

EXTRN	__chkstk:NEAR
EXTRN	ffi_closure_inner:NEAR

_TEXT	SEGMENT

;;; ffi_closure_OUTER will be called with these registers set:
;;;    rax points to 'closure'
;;;    r11 contains a bit mask that specifies which of the
;;;    first four parameters are float or double
;;;
;;; It must move the parameters passed in registers to their stack location,
;;; call ffi_closure_inner for the actual work, then return the result.
;;; 
ffi_closure_OUTER PROC FRAME
	;; save actual arguments to their stack space.
	test	r11, 1
	jne	first_is_float	
	mov	QWORD PTR [rsp+8], rcx
	jmp	second
first_is_float:
	movlpd	QWORD PTR [rsp+8], xmm0

second:
	test	r11, 2
	jne	second_is_float	
	mov	QWORD PTR [rsp+16], rdx
	jmp	third
second_is_float:
	movlpd	QWORD PTR [rsp+16], xmm1

third:
	test	r11, 4
	jne	third_is_float	
	mov	QWORD PTR [rsp+24], r8
	jmp	forth
third_is_float:
	movlpd	QWORD PTR [rsp+24], xmm2

forth:
	test	r11, 8
	jne	forth_is_float	
	mov	QWORD PTR [rsp+32], r9
	jmp	done
forth_is_float:
	movlpd	QWORD PTR [rsp+32], xmm3

done:
.ALLOCSTACK 40
	sub	rsp, 40
.ENDPROLOG
	mov	rcx, rax	; context is first parameter
	mov	rdx, rsp	; stack is second parameter
	add	rdx, 40		; correct our own area
	mov	rax, ffi_closure_inner
	call	rax		; call the real closure function
	;; Here, code is missing that handles float return values
	add	rsp, 40
	movd	xmm0, rax	; In case the closure returned a float.
	ret	0
ffi_closure_OUTER ENDP


;;; ffi_call_AMD64

stack$ = 0
prepfunc$ = 32
ecif$ = 40
bytes$ = 48
flags$ = 56
rvalue$ = 64
fn$ = 72

ffi_call_AMD64 PROC FRAME

	mov	QWORD PTR [rsp+32], r9
	mov	QWORD PTR [rsp+24], r8
	mov	QWORD PTR [rsp+16], rdx
	mov	QWORD PTR [rsp+8], rcx
.PUSHREG rbp
	push	rbp
.ALLOCSTACK 48
	sub	rsp, 48					; 00000030H
.SETFRAME rbp, 32
	lea	rbp, QWORD PTR [rsp+32]
.ENDPROLOG

	mov	eax, DWORD PTR bytes$[rbp]
	add	rax, 15
	and	rax, -16
	call	__chkstk
	sub	rsp, rax
	lea	rax, QWORD PTR [rsp+32]
	mov	QWORD PTR stack$[rbp], rax

	mov	rdx, QWORD PTR ecif$[rbp]
	mov	rcx, QWORD PTR stack$[rbp]
	call	QWORD PTR prepfunc$[rbp]

	mov	rsp, QWORD PTR stack$[rbp]

	movlpd	xmm3, QWORD PTR [rsp+24]
	movd	r9, xmm3

	movlpd	xmm2, QWORD PTR [rsp+16]
	movd	r8, xmm2

	movlpd	xmm1, QWORD PTR [rsp+8]
	movd	rdx, xmm1

	movlpd	xmm0, QWORD PTR [rsp]
	movd	rcx, xmm0

	call	QWORD PTR fn$[rbp]
ret_int$:
 	cmp	DWORD PTR flags$[rbp], 1 ; FFI_TYPE_INT
 	jne	ret_float$

	mov	rcx, QWORD PTR rvalue$[rbp]
	mov	DWORD PTR [rcx], eax
	jmp	SHORT ret_nothing$

ret_float$:
 	cmp	DWORD PTR flags$[rbp], 2 ; FFI_TYPE_FLOAT
 	jne	SHORT ret_double$

 	mov	rax, QWORD PTR rvalue$[rbp]
 	movlpd	QWORD PTR [rax], xmm0
 	jmp	SHORT ret_nothing$

ret_double$:
 	cmp	DWORD PTR flags$[rbp], 3 ; FFI_TYPE_DOUBLE
 	jne	SHORT ret_int64$

 	mov	rax, QWORD PTR rvalue$[rbp]
 	movlpd	QWORD PTR [rax], xmm0
 	jmp	SHORT ret_nothing$

ret_int64$:
  	cmp	DWORD PTR flags$[rbp], 12 ; FFI_TYPE_SINT64
  	jne	ret_nothing$

 	mov	rcx, QWORD PTR rvalue$[rbp]
 	mov	QWORD PTR [rcx], rax
 	jmp	SHORT ret_nothing$
	
ret_nothing$:
	xor	eax, eax

	lea	rsp, QWORD PTR [rbp+16]
	pop	rbp
	ret	0
ffi_call_AMD64 ENDP
_TEXT	ENDS
END
#else        
#define LIBFFI_ASM
#include <fficonfig.h>
#include <ffi.h>
.text

.extern ___chkstk
.extern _ffi_closure_inner

# ffi_closure_OUTER will be called with these registers set:
#    rax points to 'closure'
#    r11 contains a bit mask that specifies which of the
#    first four parameters are float or double
#
# It must move the parameters passed in registers to their stack location,
# call ffi_closure_inner for the actual work, then return the result.
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
	mov	_ffi_closure_inner, %rax
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
#endif /* _MSC_VER */