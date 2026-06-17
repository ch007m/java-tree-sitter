;
(element
  (STag . (Name) @tag.dep (#eq? @tag.dep "dependency"))
  (content
    (element
      (STag . (Name) @tag.g (#eq? @tag.g "groupId"))
      (content . (CharData) @group.id))
    (element
      (STag . (Name) @tag.a (#eq? @tag.a "artifactId"))
      (content . (CharData) @artifact.id))
  )) @dependency.block