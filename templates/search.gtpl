{{define "search"}}
{{template "head"}}
{{template "navi"}}

<style>
/* Form wrapper styling */
.form-wrapper {
    width: 450px;
    padding: 15px;
    margin: 100px auto 50px auto;
 
}
 
/* Form text input */
 
.form-wrapper input {
    width: 330px;
    height: 20px;
    padding: 10px 5px;
    float: left;   
    font: bold 15px 'lucida sans', 'trebuchet MS', 'Tahoma';
    border: 0;
    background: #eee;
    border-radius: 3px 0 0 3px;     
}
 
.form-wrapper input:focus {
    outline: 0;
    background: #fff;
    box-shadow: 0 0 2px rgba(0,0,0,.8) inset;
}
 
.form-wrapper input::-webkit-input-placeholder {
   color: #999;
   font-weight: normal;
   font-style: italic;
}
 
.form-wrapper input:-moz-placeholder {
    color: #999;
    font-weight: normal;
    font-style: italic;
}
 
.form-wrapper input:-ms-input-placeholder {
    color: #999;
    font-weight: normal;
    font-style: italic;
}   
 
/* Form submit button */
.form-wrapper button {
    overflow: visible;
    position: relative;
    float: right;
    border: 0;
    padding: 0;
    cursor: pointer;
    height: 40px;
    width: 110px;
    font: bold 15px/40px 'lucida sans', 'trebuchet MS', 'Tahoma';
    color: #fff;
    text-transform: uppercase;
    background: #d83c3c;
    border-radius: 0 3px 3px 0;     
    text-shadow: 0 -1px 0 rgba(0, 0 ,0, .3);
}  
   
.form-wrapper button:hover{    
    background: #e54040;
}  
   
.form-wrapper button:active,
.form-wrapper button:focus{  
    background: #c42f2f;
    outline: 0;  
}
 
.form-wrapper button:before { /* left arrow */
    content: '';
    position: absolute;
    border-width: 8px 8px 8px 0;
    border-style: solid solid solid none;
    border-color: transparent #d83c3c transparent;
    top: 12px;
    left: -6px;
}
 
.form-wrapper button:hover:before{
    border-right-color: #e54040;
}
 
.form-wrapper button:focus:before,
.form-wrapper button:active:before{
        border-right-color: #c42f2f;
}     
 
.form-wrapper button::-moz-focus-inner { /* remove extra button spacing for Mozilla Firefox */
    border: 0;
    padding: 0;
}    


.view-table
{
    display:table;
    width:100%;

}
.view-row
{
    display:table-row;
}
.view-row > div
{
    display: table-cell;
}
.view-link 
{
    text-align:left;
}
.view-id 
{
    text-align:center;
}


fieldset { overflow:hidden; padding: 0 0 0.3em 0;}
legend { float:left; clear:none; padding: 0px 2em 0 0;}
label { float:left; clear:none; display:block; padding: 0px 1em 0px 0px; }
input[type=radio], input.radio { float:left; clear:none; margin: 0px 5px 0px 0px; width: auto;}

</style> 

<form class="form-wrapper cf" id="ajaxform" action="/searchcontent" method="post">
    <fieldset>
<!--        <legend>Indeksas:</legend>-->
        <input type="radio" name="idx" value="index" checked /><label>Atvirkštinis</label>
        <input type="radio" name="idx" value="soundex" /><label>Soundex</label>
        <input type="radio" name="idx" value="vektor"/><label>Vektorinis</label>
    </fieldset>
    <fieldset id="vektor" style="display:none; font-family: inherit; font-size: small; color: cornflowerblue;">
<!--         <legend>Skaičiavimas</legend>-->
        <span style="width: 220px; height: 20px; display: inline-block;">Skaičiavimo metodas užklausai</span><span> - </span>
        <select name="qqq">
        {{range $qqqddd := .}}
            <option value='{{$qqqddd}}' {{if eq $qqqddd "ltn"}} selected="selected" {{end}}>{{$qqqddd}}</option>
        {{end}}
        </select><br>
        <span style="width: 220px; height: 20px; display: inline-block;">Skaičiavimo metodas dokumentams</span><span> - </span>
        <select name="ddd">
        {{range $qqqddd := .}}
            <option value='{{$qqqddd}}' {{if eq $qqqddd "lnc"}} selected="selected" {{end}}>{{$qqqddd}}</option>
        {{end}}
        </select>

    </fieldset>
    <input type="text" placeholder="Paieškos žodžiai" required name="searchstr">
    <button type="submit" value="Search">Ieškoti</button>
</form>

<div id="output">

</div>

<script>

$("input[name$='idx']").click(function() {
    var test = $(this).val();
    $("#vektor").hide("fast");
    $("#"+test).fadeToggle();
}); 

$("#ajaxform").submit(function(e)
{
    var postData = $(this).serializeArray();
    var formURL = $(this).attr("action");
    $.ajax(
    {
        url : formURL,
        type: "POST",
        data : postData,
        success:function(data, textStatus, jqXHR) 
        {
            $('#ajaxform').css('margin','0px auto 50px auto');
            $("#output").html(data);
        },
        error: function(jqXHR, textStatus, errorThrown) 
        {
        }
    });
    e.preventDefault(); //STOP default action

});

</script>

{{template "footer"}}
{{template "foot"}}
{{end}}