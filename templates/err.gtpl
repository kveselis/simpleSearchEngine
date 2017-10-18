{{define "err"}}
{{template "head"}}
{{template "navi"}}

{{$err := .}}

{{$err}}

{{template "foot"}}
{{end}}