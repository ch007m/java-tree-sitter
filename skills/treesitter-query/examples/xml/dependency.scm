;
(element
  (STag . (Name) @dependency (#match? @dependency "(dependency|parent)"))
  (content
    (element
      (STag . (Name) @tag.g (#eq? @tag.g "groupId"))
      (content . (CharData) @groupId))
    (element
      (STag . (Name) @tag.a (#eq? @tag.a "artifactId"))
      (content . (CharData) @artifactId))
    (element
      (STag . (Name) @tag.o (#match? @tag.o "(version|scope|optional)"))
      (content . (CharData) @version))?
  )) @dependency.block