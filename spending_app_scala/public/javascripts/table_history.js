$(function(){
    var urlParams = new URLSearchParams(location.search);
    var id = urlParams.get('user_id');
    var params = "?user_id=" + id;

    //strzaleczka
    const strzaleczka = $("#strzaleczka")
    strzaleczka.attr("href", strzaleczka.attr("href") + params);

    // const error_code = urlParams.get('err_code');
    // if (error_code && error_code === '1') {
    //     new Toast({message: urlParams.get('err_msg'), type: 'error'});
    // }
});
