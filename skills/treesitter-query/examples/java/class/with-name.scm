; Search a class name having as name:
(class_declaration
  name: (identifier) @class.name
  (#eq? @class.name "AppApplication"))

; Search a class name starting with App string
(class_declaration
  name: (identifier) @class.name
  (#match? @class.name "^App")
) @Class.name