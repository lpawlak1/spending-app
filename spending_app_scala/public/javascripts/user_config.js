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

$(function(){ // budget_form
    var urlParams = new URLSearchParams(location.search);
    var id = urlParams.get('user_id');
    var params = "?user_id=" + id;
    let budget_form = $("#modal-1 form");
    budget_form.attr("action", budget_form.attr("action") + params);
});

$(function(){
    const params = new URLSearchParams(window.location.search);
    const error_code = params.get('err_code');
    if (error_code && error_code === '1') {
        new Toast({message: params.get('err_msg'), type: 'error'});
    }
})