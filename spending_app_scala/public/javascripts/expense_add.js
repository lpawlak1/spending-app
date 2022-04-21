$(function() {
    const urlParams = new URLSearchParams(location.search);
    const id = urlParams.get('user_id');
    const params = "?user_id=" + id;

    //strzaleczka
    const strzaleczka = $("#strzaleczka")
    if (strzaleczka && params){
        strzaleczka.attr("href", strzaleczka.attr("href") + params);
    }


    // budget_form
    let budget_form = $("form");
    budget_form.attr("action", budget_form.attr("action") + params);
    const dateForDateTimeInputValue = date => new Date(date.getTime() + new Date().getTimezoneOffset() * -60 * 1000).toISOString().slice(0, 19);

    let datetime_picker = budget_form.find("input[name='datetime-local']");
    datetime_picker.value = (dateForDateTimeInputValue(new Date()));


    const error_code = urlParams.get('err_code');
    if (error_code && error_code === '1') {
        new Toast({message: urlParams.get('err_msg'), type: 'error'});
    }

    $('#add_submit').click(function(e) {
        const form = $(this).closest('form');
        const data = form.serialize();
        const url = form.attr('action');
        const params = new URLSearchParams(data);

        const required_fields = ['amount', 'category', 'name'];
        required_fields.forEach(function(field) {
            $(`#${field}-label`).removeClass('is-invalid');
        });

        params.forEach(function(value,param) {
            if (required_fields.includes(param) && !value) {
                new Toast({message: 'Fill required forms\' fields', type: 'error'});
                $(`#${param}-label`).addClass('is-invalid');
                e.preventDefault();
            }
        });
    });


})
