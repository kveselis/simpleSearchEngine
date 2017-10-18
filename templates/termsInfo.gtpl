{{define "termsInfo"}}

<div class="row">
<div class="col">
    <table>
    <caption>Nerūšiuotas<br><span class="desc">({{.RawCount}} įrašai)</span></caption>
    <thead>
        <tr>
            <th scope="col">Termas</th>
            <th scope="col">Dok.nr.</th>
        </tr>
    </thead>
    <tbody>

    {{range $u := .RawTerms}}
    <tr>
        <td>{{$u.Term}}</td>
        <td>
        {{range $b := $u.Docs}}
            {{$b.DocId}}
        {{end}}
        </td>
    </tr>
    {{end}}
    </tbody>
    </table>
</div>

<div class="col">

    <table>
    <caption>Surūšiuotas<br><span class="desc">({{.SortCount}}  įrašai)</span></caption>
    <thead>
        <tr>
            <th scope="col">Termas</th>
            <th scope="col">Dok.nr.</th>
        </tr>
    </thead>
    <tbody>

    {{range $s := .SortTerms}}
    <tr>
        <td>{{$s.Term}}</td>
        <td>
        {{range $b := $s.Docs}}
            {{$b.DocId}}
        {{end}}
        </td>
    </tr>
    {{end}}
    </tbody>
    </table>

</div>

<div class="last">
    <table>
    <caption>Sutrauktas<br><span class="desc">({{.IndexCount}}  įrašai)</span></caption>
    <thead>
        <tr>
            <th scope="col">Termas</th>
            <th scope="col">Dok.nr. - Dažnumas</th>
        </tr>
    </thead>
    <tbody>

    {{range $a := .IndexTerms}}
    <tr>
        <td>{{$a.Term}}</td>
        <td>
        {{range $b := $a.Docs}}
            <span>({{$b.DocId}} - {{$b.Freq}}) </span>
        {{end}}
        </td>
    </tr>
    {{end}}
    </tbody>
    </table>
</div>

</div>
{{end}}
