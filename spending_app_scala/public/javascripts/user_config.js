// MicroModal.init();
$(function() {
    MicroModal.init({
        onShow: modal => console.info(`${modal.id} is shown`), // [1]
        onClose: modal => console.info(`${modal.id} is hidden`), // [2]
        disableScroll: true, // [6]
        disableFocus: false, // [7]
        awaitOpenAnimation: false, // [8]
        awaitCloseAnimation: false, // [9]
        debugMode: true // [10]
    });
})

$(function(){
    var urlParams = new URLSearchParams(location.search);
    var id = urlParams.get('user_id');
    var params = "?user_id=" + id;

    // budget_form
    let budget_form = $("#modal-1 form");
    budget_form.attr("action", budget_form.attr("action") + params);

    let color_form = $("#modal-2 form");
    color_form.attr("action", color_form.attr("action") + params);

    color_form.find(".submitter").hover(function(e){
        let inttt = $(this).attr("value");
        color_form.find("#amount").val(inttt);
        console.log(inttt);
    });


    //strzaleczka
    const strzaleczka = $("#strzaleczka")
    strzaleczka.attr("href", strzaleczka.attr("href") + params);

    const error_code = urlParams.get('err_code');
    if (error_code && error_code === '1') {
        new Toast({message: urlParams.get('err_msg'), type: 'error'});
    }
});
