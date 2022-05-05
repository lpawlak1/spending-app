$(function() {
    console.log('Hello World!');
    $('#log-out').click(function() {
        window.location.href = '/login';
    });
    const urlParams = new URLSearchParams(window.location.search);
    const user_id = urlParams.get('user_id');
    $('#user-config').click(function() {
        window.location.href = `/userconfig?user_id=${user_id}`;
    });
    $("#add-new-expense-bt").click(function() {
        window.location.href = `/expense/add?user_id=${user_id}`;
    })

    $('#expense-history-table-bt').click(function() {
        window.location.href = `/history/table?user_id=${user_id}`;
    })

    $('#expense-history-chart-bt').click(function() {
        window.location.href = `/history/chart?user_id=${user_id}`;
    })

    $('#surprise-btn').click(function() {
        alert("surprise");
    })
})