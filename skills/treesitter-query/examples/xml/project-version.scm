; Grammar used: https://github.com/tree-sitter-grammars/tree-sitter-xml
; Search about Spring Boot parent OR dependency version having as groupId: org.springframework.boot
((element
   (STag . (Name) @tag.el (#match? @tag.el "^(parent|dependency)$"))
   (content
     (element
       (STag . (Name) @tag.g (#eq? @tag.g "groupId"))
       (content . (CharData) @group.id))
     (element
       (STag . (Name) @tag.a (#eq? @tag.a "artifactId"))
       (content . (CharData) @artifact.id))

     ;; Optional: version tag
     (element
       (STag . (Name) @tag.v (#eq? @tag.v "version"))
       (content . (CharData) @application.version.value))?
     )
   )
  (#eq? @group.id "org.springframework.boot")
  ) @target.element