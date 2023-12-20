# Notes

These notes are subject to change at any point for any reason without prior notice.

## Elements

ByteBuddy represents `javax.lang.model.element.TypeElement` objects as `TypeDescription`s.  It represents their
underlying types as `TypeDescription.Generic`s.

Some commonality between "elements" and "types" is stored at the `TypeDefinition` level.  `TypeDefinition` appears (to
me) to be an abstraction not really suited to adapting to the `javax.lang.model.*` classes.  Note, for example, that
`TypeDescription.Generic` (loosely equivalent to certain `TypeMirror` subclasses) overrides `getDeclaredFields` to
feature a more specific return type.  The drawback to this abstraction is that if you view the world through this lens
it is unclear what is a type _usage_ (`TypeMirror`) and what is a type _declaration_ (`Element`).

```
(TODO: convert to true tables)

javax.lang.model.* construct | Byte Buddy analog
------------------------------------------------
TypeElement | TypeDescription
ExecutableElement | MethodDescription.InDefinedShape (I think?)
DeclaredType | TypeDescription.Generic of a partiuclar Sort
```
