;
(element
  (STag . (Name) @tag.dep (#match? @tag.dep "(dependency|parent)"))
  (content
    (element
      (STag . (Name) @tag.g (#eq? @tag.g "groupId"))
      (content . (CharData) @groupId))
    (element
      (STag . (Name) @tag.a (#eq? @tag.a "artifactId"))
      (content . (CharData) @artifactId))
    [
      (element
        (STag . (Name) @tag.v (#eq? @tag.v "version"))
        (content . (CharData) @version))?
      (element
        (STag . (Name) @tag.s (#eq? @tag.s "scope"))
        (content . (CharData) @scope))?
      (element
        (STag . (Name) @tag.o (#eq? @tag.o "optional"))
        (content . (CharData) @optional))?
      ]
  )
) @dependency.block