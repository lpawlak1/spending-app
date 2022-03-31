$(function() {
    console.log('Hello World!');
    $('#log-out').click(function() {
        window.location.href = '/login';
    });
    const urlParams = new URLSearchParams(window.location.search);
    $('#user-config').click(function() {
        const user_id = urlParams.get('user_id');
        window.location.href = `/userconfig?user_id=${user_id}`;
    });
})