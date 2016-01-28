xquery version "1.0-ml";

  
let $bean-xml := fn:doc("/kbml.xml")/kbml
let $beans := $bean-xml//bean
let $class-name-from-bean := function($bean) {
  fn:replace($bean/@class, "^.*\.([^\.]+)$", "$1")
}
let $determine-property-type := function($property) {
  if ($property/valueList) then
    "List<"||($class-name-from-bean($property/valueList/bean[1]), "Object")[. ne ''][1]||">"
  else if ($property/value/@class) then
    $class-name-from-bean($property/value)
  else if ($property/value = ("True", "False")) then
    "Boolean"
  else if (fn:matches($property/value, "^[0-9]+\.[0-9]+$")) then
    "Double"
  else if (fn:matches($property/value, "^([1-9][0-9]*|[0-9])$")) then
    "Long"
  else
    "String"
    
}
let $parts := 
  for $bean in $beans 
  let $full-class-name := $bean/@class
  let $class-name := $class-name-from-bean($bean)
  let $class-package := fn:substring-before($full-class-name, "."||$class-name)
  let $property-count := fn:count($bean/property)
  let $beans-of-same-class := $beans[@class eq $full-class-name] except $bean
  where $class-name ne '' and fn:not(
      some $b in $beans-of-same-class, 
          $count in fn:count($b/property) 
      satisfies 
        $count gt $property-count 
          or 
        ($count eq $property-count and $b << $bean)
    )
  order by $bean/@class
  return (
    element {fn:QName("xdmp:zip", "part")} {fn:replace($bean/@class, "\.", "/") || ".java"},
    document {
      "package " || $class-package || ";&#10;&#10;" ||
      "import  java.util.List;&#10;"||
      fn:string-join(
        for $class in fn:distinct-values($bean//*/@class)
        where fn:not(fn:starts-with($class, $class-package) and fn:matches(fn:substring-after($class, $class-package), "^\.[^\.]+$"))
        order by $class
        return 
          "import " || $class || ";",
        "&#10;"
      )|| "&#10;&#10;"||
      "import com.marklogic.client.pojo.annotation.Id;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;&#10;&#10;"||
      '@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "'|| fn:lower-case(fn:substring($class-name, 1, 1)) || fn:substring($class-name, 2) ||'")
@XmlType(name = "'||$class-name||'", propOrder = {&#10;'||
fn:string-join(
  for $property-name in $bean/property/@name
  return
    '  "' || $property-name || '"',
  ",&#10;"
) || '})&#10;'||
      "public class "|| $class-name || " { &#10;"||
      fn:string-join(
      for $property in $bean/property
      let $property-type := $determine-property-type($property)
      return
        (if (fn:matches($property-type, "^List<.*>$")) then
          '  @XmlElement(name = "' || fn:replace($property/@name, 'List$', '') || '") &#10;'||
          "  @XmlElementWrapper&#10;"
        else
          "  @XmlElement&#10;")||
        (if ($property/@name = "uniqueObjectKey") then
          "  @Id&#10;"
        else "")||
        "  public " || $property-type || " " || $property/@name || ";"
      , "&#10;") || "&#10;&#10;" ||
      fn:string-join(
      for $property in $bean/property
      let $property-type :=  $determine-property-type($property)
      let $cap-name := fn:upper-case(fn:substring($property/@name, 1, 1)) || fn:substring($property/@name, 2)
      return
        "  public " || $property-type || " get" || $cap-name || "() { &#10;" ||
        "    return this." || $property/@name || "; &#10;" ||
        "  }&#10;&#10;" ||
        "  public void set" || $cap-name || "(" || $property-type || " "|| $property/@name ||") { &#10;" ||
        "    this." || $property/@name || " = "|| $property/@name ||"; &#10;" ||
        "  }&#10;&#10;"
      , "&#10;") || "}"
    }
  )
let $zip := xdmp:zip-create(
               <parts xmlns="xdmp:zip">
                 {$parts[self::element()]}
                </parts>,
                $parts[self::document-node()]
            )
return 
  xdmp:save("/Users/rdew/space/mycompany-pojos-java.zip", $zip,
     <options xmlns="xdmp:save">
        <encoding>utf8</encoding>
     </options>)
