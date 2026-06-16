; (element (STag (Name) @tag.dep (#eq? @tag.dep "dependency"))
;   (content (CharData) @content
;     (element (STag (Name) (content (CharData)) @tag.groupId (ETag (Name))))
;     (element (STag (Name) @tag.artifactId))
;     (element (STag (Name) @tag.version))
;   )
; )

(element
  (STag (Name) @tag.dep (#eq? @tag.dep "dependency"))
  (content
    (element
      (STag (Name) @tag.g (#eq? @tag.g "groupId"))
      (content (CharData) @group.id))
    (element
      (STag (Name) @tag.a (#eq? @tag.a "artifactId"))
      (content (CharData) @artifact.id))
    (element
      (STag (Name) @tag.v (#eq? @tag.v "version"))
      (content (CharData) @artifact.version))
    )
  ) @dependency.block