xquery version "1.0-ml";

declare function local:class-name-from-QName($qname as xs:QName) 
{
  let $string := (
      fn:local-name-from-QName($qname),
      fn:string($qname)
    )[. ne ""][1]
  return
    fn:string-join(
      for $part in fn:tokenize($string, "[^a-zA-Z0-9]")
      return
        fn:upper-case(fn:substring($part, 1, 1)) || fn:substring($part, 2),
      ""
    )
};


declare function local:get-sample-docs(
    $amount as xs:integer,
    $unique-qnames as xs:QName*
) as node()*
{
  let $sample-positions as xs:integer+ := (1 to $amount)
  let $unique-root-qnames as xs:QName* := (local:find-unique-root-qnames("xml"), local:find-unique-root-qnames("json"))
  for $root-qname as xs:QName in $unique-root-qnames
  return
    (
      cts:search(fn:collection()/element(),
        cts:element-query($root-qname,cts:and-query(()),"self"),
        "format-xml"
      )[fn:position() = $sample-positions],
      cts:search(fn:collection()/*,
        cts:json-property-scope-query(fn:string($root-qname), cts:and-query(())),
        "format-json"
      )[fn:position() = $sample-positions]
    )
};

declare variable $sample-positions as xs:integer+ := (1 to 5);

declare function local:find-unique-root-qnames($format as xs:string?, $found-qnames as xs:QName*) {
  let $next-qname := fn:distinct-values(cts:search(fn:collection(),
      if (fn:exists($found-qnames))
      then
        if ($format = "json") then
          cts:not-query(cts:json-property-scope-query(($found-qnames ! fn:string(.)), cts:and-query(())))
        else
          cts:not-query(cts:element-query($found-qnames,cts:and-query(()),"self"))
      else cts:and-query(()),
      (
        if (fn:exists($format)) then
          "format-" || $format
        else (),
        "score-random",
        "unfaceted",
        "filtered"
      )
    )[fn:position() = $sample-positions]/(.|object-node())/node()/fn:node-name(.))
  return if (fn:exists($next-qname))
          then local:find-unique-root-qnames($format, ($found-qnames,$next-qname))
          else $found-qnames
};

declare function local:find-unique-root-qnames($format as xs:string?) {
  for $qn in local:find-unique-root-qnames($format, ())
  order by string($qn)
  return $qn
};

declare function local:json-property-type($sample-set as node()*, $property as item()) {
  if ($property instance of array-node()) then
    "List<"||(local:json-property-type($sample-set, $property/node()[1]), "Object")[. ne ''][1]||">"
  else if ($property instance of object-node()) then
    local:class-name-from-QName(fn:node-name($property))
  else if ($property instance of boolean-node()) then
    "Boolean"
  else if ($property instance of number-node()) then
    "Double"
  else if ($property castable as xs:date or $property castable as xs:dateTime) then
    "GregorianCalendar"
  else
    "String"
};

declare function local:xml-property-type($sample-set as node()*, $property as item()) {
  let $property-details := local:determine-xml-details($sample-set, fn:node-name($property))
  let $different-types := $property-details/types
  let $count-of-types := fn:count($different-types)
  let $type := 
    if ($count-of-types eq 2 and $different-types/type = "string") then
      $different-types[type ne "string"]
    else
      fn:head($different-types/type)
  return
    if ($type eq "[Array]") then
      "List<"||(local:xml-property-type($sample-set, $property/*[1]), "Object")[. ne ''][1]||">"
    else if ($type eq "[Object]") then
      local:class-name-from-QName(fn:node-name($property))
    else if ($type eq "boolean") then
      "Boolean"
    else if ($type = ("integer", "decimal", "double")) then
      "Double"
    else if ($type eq "float") then
      "Float"
    else if ($type = ("date", "dateTime")) then
      "GregorianCalendar"
    else
      "String"
};

declare function local:determine-xml-details($sample-set as node()*, $property-name as xs:QName)
{
  let $properties := $sample-set/descendant-or-self::node()[fn:node-name(.) eq $property-name]
  let $all-have-children := every $prop in $properties satisfies fn:exists($prop/*)
  let $some-have-children := fn:exists($properties/*)
  return
    object-node {
      "propertyName": fn:string($property-name),
      "types": array-node {
          let $prop-types :=
            for $property in $properties
            let $children := $property/*
            let $is-array-like := fn:count(fn:distinct-values($children/fn:node-name(.))) eq 1
            return
              if ($is-array-like) then
                "[Array]"
              else if (fn:exists($children)) then
                "[Object]"
              else
                local:determine-xml-type($property)
          for $prop-type in fn:distinct-values($prop-types)
          let $count := fn:count($prop-types[. eq $prop-type])
          order by $count descending
          return
            object-node {
              "type": $prop-type,
              "count": $count
            }

        },
      "allHaveChildren": $all-have-children,
      "someHaveChildren": $some-have-children
    }
};

declare function local:determine-xml-type($property as node())
{
  if (xdmp:type($property) ne xs:QName("xs:untypedAtomic")) then
    fn:string(xdmp:type($property))
  else
    local:determine-xml-type(
      $property, 
      (: Types to check if castable as. string is last since that will match anything :)
      (
        "time",
        "date",
        "dateTime",
        "integer",
        "decimal",
        "double",
        "float",
        "dayTimeDuration",
        "yearMonthDuration",
        "string"
      )
    )
};


declare function local:determine-xml-type($property as node(), $types as xs:string*)
{
  if (fn:empty($types)) then
    "untypedAtomic"
  else if (xdmp:castable-as("http://www.w3.org/2001/XMLSchema", fn:head($types), $property)) then
    fn:head($types)
  else
    local:determine-xml-type($property, fn:tail($types))
};

declare function local:xml-imports()
{
"import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;&#10;&#10;"
};

declare function local:json-imports()
{
"import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;&#10;&#10;"
};

declare function local:xml-class-annotations($class-name as xs:string, $properties as xs:QName*)
{
'@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "'|| fn:lower-case(fn:substring($class-name, 1, 1)) || fn:substring($class-name, 2) ||'")
@XmlType(name = "'||$class-name||'", propOrder = {&#10;'||
fn:string-join(
  for $property in ($properties ! fn:local-name-from-QName(.))
  return
    '  "' || $property || '"',
  ",&#10;"
) || '})&#10;'
};

declare function local:json-class-annotations($class-name as xs:string, $properties as xs:QName*)
{
'@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName("'||fn:lower-case(fn:substring($class-name, 1, 1)) || fn:substring($class-name, 2)||'")
@JsonInclude(JsonInclude.Include.NON_NULL)&#10;'
};

declare function local:json-prop-annotations($name as xs:QName, $property-type as xs:string)
{
'  @JsonProperty("'||fn:local-name-from-QName($name)||'")&#10;'
};

declare function local:xml-prop-annotations($name as xs:QName, $property-type as xs:string)
{
  '  @XmlElement(namespace = "' || fn:namespace-uri-from-QName($name) || '", name = "' || fn:local-name-from-QName($name) || '") &#10;'||
  (if (fn:matches($property-type, "^List<.*>$")) then
    "  @XmlElementWrapper&#10;"
  else 
    "")
};

declare variable $json-config := map:new((
    map:entry("imports", xs:QName('local:json-imports')),
    map:entry("class-annotations", xs:QName('local:json-class-annotations')),
    map:entry("prop-annotations", xs:QName('local:json-prop-annotations')),
    map:entry("property-type", xs:QName('local:json-property-type'))
  ));

declare variable $xml-config := map:new((
    map:entry("imports", xs:QName('local:xml-imports')),
    map:entry("class-annotations", xs:QName('local:xml-class-annotations')),
    map:entry("prop-annotations", xs:QName('local:xml-prop-annotations')),
    map:entry("property-type", xs:QName('local:xml-property-type'))
  ));

declare variable $class-package := "com.mycompany.app.pojos";

declare variable $basic-types := ("Boolean", "Double", "Long", "Float", "GregorianCalendar", "String");

declare function local:produce-classes($qnames as xs:QName*, $sample-documents as node()*, $config as map:map) 
{
  let $import-fun := fn:function-lookup(map:get($config, "imports"), 0)
  let $class-annotations-fun := fn:function-lookup(map:get($config, "class-annotations"), 2)
  let $prop-annotations-fun := fn:function-lookup(map:get($config, "prop-annotations"), 2)
  let $property-type-fun := fn:function-lookup(map:get($config, "property-type"), 2)
  for $class-qname in $qnames 
  let $class-sample-documents := $sample-documents[fn:node-name(.) eq $class-qname]
  let $class-name := local:class-name-from-QName($class-qname)
  let $full-class-name := $class-package || "." || $class-name
  let $properties := fn:distinct-values($class-sample-documents/node() ! fn:node-name(.))
  order by $class-name
  return (
    for $property in $properties
    let $sample-props := $class-sample-documents/node()[fn:node-name(.) eq $property]
    let $first-prop := 
      if (fn:head($sample-props) instance of array-node()) then
        fn:head($sample-props)/node()[1]
      else
        fn:head($sample-props)
    where fn:not($property-type-fun($class-sample-documents, $first-prop) = $basic-types)
    return
      local:produce-classes($property, $sample-props, $config),
    element {fn:QName("xdmp:zip", "part")} {fn:replace($full-class-name, "\.", "/") || ".java"},
    document {
      "package " || $class-package || ";&#10;&#10;" ||
      "import  java.util.List;&#10;"||
      "&#10;"||
      "import com.marklogic.client.pojo.annotation.Id;

import java.io.Serializable; 
"|| 
  $import-fun() ||
  $class-annotations-fun($class-name, $properties)
||
      "public class "|| $class-name || " { &#10;"||
      fn:string-join(
      for $property in $properties
      let $first-prop := ($class-sample-documents/node()[fn:node-name(.) eq $property])[1]
      let $property-name := fn:local-name-from-QName($property)
      let $property-type := $property-type-fun($class-sample-documents, $first-prop)
      return
        ($prop-annotations-fun($property, $property-type))||
        (
          if (fn:tokenize(xdmp:node-uri($first-prop), "[/\.]")[. ne ""] = fn:string($first-prop)) then
            "  @Id&#10;"
          else
            ""
          )||
        "  public " || $property-type || " " || $property-name || ";"
      , "&#10;") || "&#10;&#10;" ||
      fn:string-join(
      for $property in $properties
      let $first-prop := ($class-sample-documents/node()[fn:node-name(.) eq $property])[1]
      let $property-name := fn:local-name-from-QName($property)
      let $property-type := $property-type-fun($class-sample-documents, $first-prop)
      let $cap-name := fn:upper-case(fn:substring($property-name, 1, 1)) || fn:substring($property-name, 2)
      return
        "  public " || $property-type || " get" || $cap-name || "() { &#10;" ||
        "    return this." || $property-name || "; &#10;" ||
        "  }&#10;&#10;" ||
        "  public void set" || $cap-name || "(" || $property-type || " "|| $property-name ||") { &#10;" ||
        "    this." || $property-name || " = "|| $property-name ||"; &#10;" ||
        "  }&#10;&#10;"
      , "&#10;") || "}"
    }
  )
};


let $sample-size := 5  
let $xml-qnames := local:find-unique-root-qnames("xml")
let $json-qnames := local:find-unique-root-qnames("json")
let $json-sample-documents := local:get-sample-docs($sample-size, $json-qnames)
let $xml-sample-documents := local:get-sample-docs($sample-size, $xml-qnames)

let $parts := 
  (
    local:produce-classes($xml-qnames, $xml-sample-documents, $xml-config), 
    local:produce-classes($json-qnames, $json-sample-documents, $json-config)
  )
let $manifest-parts := $parts[self::element()]
let $document-parts := $parts[self::document-node()]
let $valid-indexes := 
  for $p at $pos in $manifest-parts
  where every $following in fn:subsequence($manifest-parts, $pos + 1) satisfies $following ne $p
  return $pos
let $zip := xdmp:zip-create(
               <parts xmlns="xdmp:zip">
                 {$manifest-parts[fn:position() = $valid-indexes]}
                </parts>,
                $document-parts[fn:position() = $valid-indexes]
            )
return ($parts,
  xdmp:save("/Users/rdew/space/my-company-java-pojos.zip", $zip,
     <options xmlns="xdmp:save">
        <encoding>utf8</encoding>
     </options>)
)