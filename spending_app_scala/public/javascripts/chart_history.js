let start_;
let end_;
let id;
let seeDeleted_ = false;
let cat_id_ = -1;

let chart_1;

function reload() {
    $.getJSON(`/history/get_row?user_id=${id}&start_date=${start_.format('DD.MM.YYYY')}&end_date=${end_.format('DD.MM.YYYY')}&del=${seeDeleted_.toString()}&category_id=${cat_id_}`, (res => {
        // for (const expense of res) {
        //     if (expense.deleted === 'false') {
        //             <td class="hidden">${expense.id}</td>
        //             <td class="table__name">${expense.name}</td>
        //             <td class="table__date">${expense.purchase_date}</td>
        //             <td class="table__price">${expense.price}</td>
        //             <td class="table__description">${expense.desc}</td>

        const data = res.map(expense => {return {y: expense.price, x: new Date(expense.purchase_date).getMonth()}});

        console.log(res.map(expense => {return new Date(expense.purchase_date).getMonth()}));


        let chart = new Chart(chart_1, {
            type: 'line',
            data: {
                datasets: [{
                    // labels: data.map(expense => new Date(expense.x).getDay()),
                    data: data,
                    backgroundColor: [
                        'rgba(255, 159, 64, 1)'
                    ],
                    borderColor: [
                        'rgba(255, 99, 132, 1)'
                    ]
                }],
            },
            options: {
                scales: {
                    x: {
                        ticks: {
                            autoSkip: false,
                            maxRotation: 0,
                            major: {
                                enabled: true
                            },
                        }
                        // max: '2021-11-07 00:00:00',
                    }
                }
            }
        });

        // const myChart = new Chart(chart_1, {
        //     type: 'line',
        //     data: {
        //         labels: labels,
        //         backgroundColor: 'rgba(255, 255, 255, 1)',
        //         datasets: [{
        //             label: 'Your expenses',
        //             data: data,
        //                 'rgba(54, 162, 235, 1)',
        //                 'rgba(255, 206, 86, 1)',
        //                 'rgba(75, 192, 192, 1)',
        //                 'rgba(153, 102, 255, 1)',
        //                 'rgba(255, 159, 64, 1)'
        //             ],
        //             borderWidth: 1
        //         }]
        //     },
        //     options: {
        //         scales: {
        //             x: {
        //             }
        //         }
        //     }
        // });
    }));
}

$(function(){
    chart_1 = $('#first_chart');

    const urlParams = new URLSearchParams(location.search);
    id = urlParams.get('user_id');
    const params = "?user_id=" + id;

    //strzaleczka
    const strzaleczka = $("#strzaleczka")
    strzaleczka.attr("href", strzaleczka.attr("href") + params);

    $('#datepicker').daterangepicker({
        ranges: {
            'Today': [moment(), moment()],
            // 'Yesterday': [moment().subtract(1, 'days'), moment().subtract(1, 'days')],
            'Last 7 Days': [moment().subtract(6, 'days'), moment()],
            'Last 30 Days': [moment().subtract(29, 'days'), moment()],
            'This Month': [moment().startOf('month'), moment().endOf('month')],
            'Last Month': [moment().subtract(1, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')]
        },
        startDate: moment().startOf('month'),
        endDate: moment().endOf('month'),
        locale: {
            direction: 'ltr',
            format: moment.localeData().longDateFormat('L'),
            separator: ' - ',
            applyLabel: 'Apply',
            cancelLabel: 'Cancel',
            weekLabel: 'W',
            customRangeLabel: 'Custom Range',
            daysOfWeek: moment.weekdaysMin(),
            monthNames: moment.monthsShort(),
            firstDay: moment.localeData().firstDayOfWeek()
        },

    }, function(start, end, label) {
        start_ = start;
        end_ = end;
        reload();
    });
    start_ = moment().startOf('month')
    end_ = moment().endOf('month')

    $("#showDeleted").change(function() {
        seeDeleted_ = $("#showDeleted").is(':checked');
        reload();
    });

    $("input[name=category]").change(function() {
        cat_id_ = $(this).val();
        reload();
    });

    reload();
});
