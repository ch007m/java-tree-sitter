; Grammar used: https://github.com/panicinc/tree-sitter-xml
; Search about Spring Boot parent OR dependency version having as groupId: org.springframework.boot
((element
   (start_tag (tag_name) @tag.el (#match? @tag.el "^(parent|dependency)$"))
   (element
     (start_tag (tag_name) @tag.g (#eq? @tag.g "groupId"))
     (text) @group.id)
   (element
     (start_tag (tag_name) @tag.a (#eq? @tag.a "artifactId"))
     (text) @artifact.id)

   ;; Optional: parent always has a version, dependency may inherit it
   (element
     (start_tag (tag_name) @tag.v (#eq? @tag.v "version"))
     (text) @application.version.value)?
   )
  (#eq? @group.id "org.springframework.boot")
  ) @target.element