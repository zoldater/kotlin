// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION
// !WITH_NEW_INFERENCE

abstract class A<Context : A<Context>> {
    val x: X = TODO()

    fun getGetX(): X = TODO()
}

class B<D> : A<B<D>>()

//class C : A<C>()

interface X

fun B<*>.checkValueArguments() {
//    <!DEBUG_INFO_EXPRESSION_TYPE("B<*>")!>this<!>
//    <!DEBUG_INFO_EXPRESSION_TYPE("X")!>this.x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("X")!>x<!>

//    <!DEBUG_INFO_EXPRESSION_TYPE("X")!>this.getGetX()<!>
//    <!DEBUG_INFO_EXPRESSION_TYPE("X")!>getGetX()<!>
}

//fun C.test() {
//    <!DEBUG_INFO_EXPRESSION_TYPE("X")!>x<!>
//}

/*
compiler/frontend/src/org/jetbrains/kotlin/resolve/calls/CandidateResolver.kt:96:13: error: unresolved reference. None of the following candidates is applicable because of receiver type mismatch:
@NotNull public final val call: Call defined in org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext
        if (call.typeArguments.isEmpty()
            ^
compiler/frontend/src/org/jetbrains/kotlin/resolve/calls/CandidateResolver.kt:107:31: error: unresolved reference. None of the following candidates is applicable because of receiver type mismatch:
@NotNull public final val call: Call defined in org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext
        val ktTypeArguments = call.typeArguments
                              ^
compiler/frontend/src/org/jetbrains/kotlin/resolve/calls/CandidateResolver.kt:115:60: error: unresolved reference. None of the following candidates is applicable because of receiver type mismatch:
@NotNull public final val trace: BindingTrace defined in org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext
                val type = projection.typeReference?.let { trace.bindingContext.get(BindingContext.TYPE, it) }
                                                           ^
compiler/frontend/src/org/jetbrains/kotlin/resolve/calls/CandidateResolver.kt:115:106: error: unresolved reference: it
                val type = projection.typeReference?.let { trace.bindingContext.get(BindingContext.TYPE, it) }
                                                                                                         ^
compiler/frontend/src/org/jetbrains/kotlin/resolve/calls/CandidateResolver.kt:133:52: error: unresolved reference. None of the following candidates is applicable because of receiver type mismatch:
@NotNull public final val trace: BindingTrace defined in org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext
                tracing.wrongNumberOfTypeArguments(trace, expectedTypeArgumentCount, candidateDescriptor)
                                                   ^
compiler/frontend/src/org/jetbrains/kotlin/resolve/calls/CandidateResolver.kt:135:117: error: unresolved reference. None of the following candidates is applicable because of receiver type mismatch:
@NotNull public final val trace: BindingTrace defined in org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext
                checkGenericBoundsInAFunctionCall(ktTypeArguments, typeArguments, candidateDescriptor, substitutor, trace)
                                                                                                                    ^
compiler/frontend/src/org/jetbrains/kotlin/resolve/calls/CandidateResolver.kt:187:98: error: unresolved reference. None of the following candidates is applicable because of receiver type mismatch:
@NotNull public final val scope: LexicalScope defined in org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext
            getReceiverValueWithSmartCast(receiverArgument, smartCastType), candidateDescriptor, scope.ownerDescriptor
                                                                                                 ^
compiler/frontend/src/org/jetbrains/kotlin/resolve/calls/CandidateResolver.kt:190:37: error: unresolved reference. None of the following candidates is applicable because of receiver type mismatch:
@NotNull public final val trace: BindingTrace defined in org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext
            tracing.invisibleMember(trace, invisibleMember)
                                    ^
compiler/frontend/src/org/jetbrains/kotlin/resolve/calls/CandidateResolver.kt:208:30: error: unresolved reference. None of the following candidates is applicable because of receiver type mismatch:
@NotNull public final val scope: LexicalScope defined in org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext
        candidateDescriptor, scope.ownerDescriptor
                             ^
compiler/frontend/src/org/jetbrains/kotlin/resolve/calls/CandidateResolver.kt:219:17: error: unresolved reference. None of the following candidates is applicable because of receiver type mismatch:
@NotNull public final val call: Call defined in org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext
            if (call.calleeExpression is KtSimpleNameExpression) {
                ^
compiler/frontend/src/org/jetbrains/kotlin/resolve/calls/CandidateResolver.kt:242:63: error: unresolved reference. None of the following candidates is applicable because of receiver type mismatch:
@NotNull public final val trace: BindingTrace defined in org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext
                tracing.nestedClassAccessViaInstanceReference(trace, nestedClass, candidateCall.explicitReceiverKind)
                                                              ^
compiler/frontend/src/org/jetbrains/kotlin/resolve/calls/CandidateResolver.kt:280:58: error: unresolved reference. None of the following candidates is applicable because of receiver type mismatch:
@NotNull public final val trace: BindingTrace defined in org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext
                    tracing.instantiationOfAbstractClass(trace)
                                                         ^
compiler/frontend/src/org/jetbrains/kotlin/resolve/calls/CandidateResolver.kt:288:43: error: unresolved reference. None of the following candidates is applicable because of receiver type mismatch:
@NotNull public final val trace: BindingTrace defined in org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext
                tracing.abstractSuperCall(trace)
                                          ^
compiler/frontend/src/org/jetbrains/kotlin/resolve/calls/CandidateResolver.kt:297:13: error: unresolved reference. None of the following candidates is applicable because of receiver type mismatch:
@NotNull public final val trace: BindingTrace defined in org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext
            trace.report(SUPER_CANT_BE_EXTENSION_RECEIVER.on(superExtensionReceiver, superExtensionReceiver.text))
            ^
compiler/frontend/src/org/jetbrains/kotlin/resolve/calls/CandidateResolver.kt:307:17: error: unresolved reference. None of the following candidates is applicable because of receiver type mismatch:
@NotNull public final val trace: BindingTrace defined in org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext
                trace.report(EXPANDED_TYPE_CANNOT_BE_CONSTRUCTED.on(call.callElement, descriptor.returnType))
                ^
compiler/frontend/src/org/jetbrains/kotlin/resolve/calls/CandidateResolver.kt:307:69: error: unresolved reference. None of the following candidates is applicable because of receiver type mismatch:
@NotNull public final val call: Call defined in org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext
                trace.report(EXPANDED_TYPE_CANNOT_BE_CONSTRUCTED.on(call.callElement, descriptor.returnType))
                                                                    ^
compiler/frontend/src/org/jetbrains/kotlin/resolve/calls/CandidateResolver.kt:422:56: error: unresolved reference. None of the following candidates is applicable because of receiver type mismatch:
@NotNull public final val call: Call defined in org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext
        if (!isInvokeCallOnExpressionWithBothReceivers(call)) {
                                                       ^
compiler/frontend/src/org/jetbrains/kotlin/resolve/calls/CandidateResolver.kt:667:73: error: unresolved reference. None of the following candidates is applicable because of receiver type mismatch:
@NotNull public final val call: Call defined in org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext
        val reportStrategy = TypeAliasSingleStepExpansionReportStrategy(call.callElement, typeAliasDescriptor, ktTypeArguments, trace)
 */