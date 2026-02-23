package chat.schildi.revenge.config.keybindings

typealias CommandArgContext = List<Pair<ActionArgument, String>>

fun CommandArgContext.findAll(primitive: ActionArgumentPrimitive) = mapNotNull { (ctxArgDef, ctxArgVal) ->
    ctxArgVal.takeIf { ctxArgDef.canHold(primitive) }
}

fun CommandArgContext.getParameter(primitive: ActionArgumentPrimitive) = mapNotNull { (ctxArgDef, ctxArgVal) ->
    ctxArgVal.takeIf { ctxArgDef.canHold(primitive) }
}.takeIf { it.size == 1 }?.first()
