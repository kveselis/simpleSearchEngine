{{define "termsPage"}}
{{template "head"}}
{{template "navi"}}

<style>

.col
{
    float: left;
    width: 30%;
}

.last{
    float: right;
    width: 40%;
}
.row{

    height: auto;
    overflow: auto;
}

.desc{
   font-family: "Proxima Nova Regular","Helvetica Neue",Calibri,"Droid Sans",Helvetica,Arial,sans-serif;
   font-style: italic;
   font-size: 0.8em;
   color: rgba(0, 0, 0, 0.3);
   line-height: 1;
}


</style>
<h3>{{.Title}}<img src="/css/more.svg" onclick="location.href=location.href + '/all'" style="width:32px; padding-left:15px; cursor: pointer;" title="Rodyti pilną sąrašą"></h3>

<div id="output">

{{template "termsInfo" .}}

</div>

{{template "footer"}}
{{template "foot"}}
{{end}}