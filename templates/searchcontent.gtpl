{{define "searchcontent"}}
    <table class="center">
    <caption>Paieškos <span class="comment">'{{.Sstring}}'</span> rezultatas</caption>
    <thead>
        <tr>
            <th scope="col">Dokumento<br/>ID</th>
            <th scope="col">Nuoroda</th>
        </tr>
    </thead>
    <tfoot>
        <tr>
            <td></td>
            <td></td>
        </tr>
    </tfoot>
    <tbody>
    {{range $r := .Sres}}
    <tr>
        <td style="text-align: center;">{{$r.Docid}}</td>
        <td><a class="inline-link-2" href='{{$r.Url}}'>{{$r.Title}}</a></td>
    </tr>
    {{end}}
    </tbody>

	</table>
{{end}}