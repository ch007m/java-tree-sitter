; Slow response time using as grammar: https://github.com/tree-sitter-grammars/tree-sitter-xml
; (element
;   (STag (Name) @tag.dep (#eq? @tag.dep "dependency"))
;   (content
;     (element
;       (STag (Name) @tag.g (#eq? @tag.g "groupId"))
;       (content (CharData) @group.id))
;     (element
;       (STag (Name) @tag.a (#eq? @tag.a "artifactId"))
;       (content (CharData) @artifact.id))
;     (element
;       (STag (Name) @tag.v (#eq? @tag.v "version"))
;       (content (CharData) @artifact.version))
;     )
;   ) @dependency.block

; Grammar used: https://github.com/panicinc/tree-sitter-xml
; Search about Spring Boot dependencies
((element
   (start_tag (tag_name) @tag.dep (#eq? @tag.dep "dependency"))
   (element
     (start_tag (tag_name) @tag.g (#eq? @tag.g "groupId"))
     (text) @group.id)
   (element
     (start_tag (tag_name) @tag.a (#eq? @tag.a "artifactId"))
     (text) @artifact.id)

   ;; Optional modifier (?) applied to the whole version element block
   (element
     (start_tag (tag_name) @tag.v (#eq? @tag.v "version"))
     (text) @version.value)?
   )
  (#eq? @group.id "org.springframework.boot")
  ) @target.dependency