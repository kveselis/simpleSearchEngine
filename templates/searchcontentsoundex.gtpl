{{define "searchcontentsoundex"}}
    <table class="center" style="min-width:350px;">
    <caption>Paieškos <span class="comment">'{{.Sstring}}'</span> rezultatas</caption>
    <thead>
        <tr>
            <th scope="col">Žodis</th>
            <th scope="col">Soundex</th>
        </tr>
    </thead>
    <tfoot>
        <tr>
            <td></td>
            <td></td>
        </tr>
    </tfoot>
    <tbody>
    {{range $key, $val := .Sres}}
    <tr>
        <td>{{$key}}</td>
        <td>{{$val}}</td>
    </tr>
    {{end}}
    </tbody>

	</table>
{{end}}