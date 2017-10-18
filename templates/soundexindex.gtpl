{{define "soundexindex"}}


<style>

.col
{
    float: left;
    width: 30%;
}

.first{
    float: left;
    width: 35%;
}
.last{
    float: right;
    width: 35%;
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

table{
	
}

</style>

<div class="row">
<div class="first">
    <table>
    <caption>Termai<br><span class="desc">({{.IndexCount}}  įrašai)</span></caption>
    <thead>
        <tr>
            <th scope="col">Termas</th>
            <th scope="col">Dok.</th>
        </tr>
    </thead>
    <tbody>

    {{range $a := .IndexTerms}}
    <tr>
        <td>{{$a.Term}}</td>
        <td>
        {{range $b := $a.Docs}}
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
    <caption>Soundex indeksas<br><span class="desc">({{.IndexCount}} įrašai)</span></caption>
    <thead>
        <tr>
            <th scope="col">Termas</th>
            <th scope="col">Kodas</th>
        </tr>
    </thead>
    <tbody>

    {{range $key, $val := .Sndx}}
    <tr>
        <td>{{$key}}</td>
        <td>{{$val}}</td>
    </tr>
    {{end}}
    </tbody>
    </table>
</div>
<div class="last">
    <table>
    <caption>Soundex algoritmas<br><span class="desc">Kodavimas</span></caption>
    <thead>
        <tr>
            <th scope="col">Simbolis</th>
            <th scope="col">Kodas</th>
        </tr>
    </thead>
    <tbody>
    <tr>
        <td>'A', 'Ą', 'E', 'Ę', 'Ė', 'I', 'Į', 'O', 'U', 'Ų', 'Ū', 'Y'</td>
        <td>0</td>
    </tr>
    <tr>
        <td>'P', 'B'</td>
        <td>1</td>
    </tr>
    <tr>
        <td>'C', 'Č'</td>
        <td>2</td>
    </tr>
    <tr>
        <td>'D', 'T'</td>
        <td>3</td>
    </tr>
    <tr>
        <td>'F', 'V', 'H', 'W', 'X'</td>
        <td>4</td>
    </tr>
    <tr>
        <td>'G', 'K'</td>
        <td>5</td>
    </tr>
    <tr>
        <td>'L', 'J'</td>
        <td>6</td>
    </tr>
    <tr>
        <td>'N', 'M'</td>
        <td>7</td>
    </tr>
    <tr>
        <td>'R'</td>
        <td>8</td>
    </tr>
    <tr>
        <td>'S', 'Š', 'Z', 'Ž'</td>
        <td>9</td>
    </tr>


    </tbody>
    </table>
</div>

</div>

{{end}}