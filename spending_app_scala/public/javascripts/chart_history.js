let start_;
let end_;
let id;
let seeDeleted_ = false;
let cat_id_ = -1;

let chart_1;
let chart_1_main;

let chart_2;
let chart_2_main;

function reload_chart_1() {
    $.getJSON(`/history/get_row?user_id=${id}&start_date=${start_.format('DD.MM.YYYY')}&end_date=${end_.format('DD.MM.YYYY')}&del=${seeDeleted_.toString()}&category_id=${cat_id_}`, (res => {

        const data = res.map(expense => {return {y: expense.price, x: new Date(expense.purchase_date)}});

        const totalDuration = 1000;
        const delayBetweenPoints = totalDuration / data.length;
        const previousY = (ctx) => ctx.index === 0 ? ctx.chart.scales.y.getPixelForValue(100) : ctx.chart.getDatasetMeta(ctx.datasetIndex).data[ctx.index - 1].getProps(['y'], true).y;
        const animation = {
            x: {
                type: 'number',
                easing: 'linear',
                duration: delayBetweenPoints,
                from: NaN, // the point is initially skipped
                delay(ctx) {
                    if (ctx.type !== 'data' || ctx.xStarted) {
                        return 0;
                    }
                    ctx.xStarted = true;
                    return ctx.index * delayBetweenPoints;
                }
            },
            y: {
                type: 'number',
                easing: 'linear',
                duration: delayBetweenPoints,
                from: previousY,
                delay(ctx) {
                    if (ctx.type !== 'data' || ctx.yStarted) {
                        return 0;
                    }
                    ctx.yStarted = true;
                    return ctx.index * delayBetweenPoints;
                }
            }
        };
        if (chart_1_main !== undefined) {
            chart_1_main.data.datasets[0].data = data;
            chart_1_main.update();
        }
        else {
            chart_1_main = new Chart(chart_1, {
                type: 'line',
                data: {
                    datasets: [{
                        data: data,
                        title: 'Expenses',
                        backgroundColor: [
                            'rgba(255, 159, 64, 1)'
                        ],
                        borderColor: [
                            'rgba(255, 99, 132, 1)'
                        ]
                    }],
                },
                options: {
                    plugins: {
                        legend: {
                            display: false
                        }
                    },
                    scales: {
                        x: {
                            type: 'time',
                            ticks: {
                                autoSkip: false,
                                maxRotation: 0,
                                major: {
                                    enabled: true
                                },
                                // color: function(context) {
                                //   return context.tick && context.tick.major ? '#FF0000' : 'rgba(0,0,0,0.1)';
                                // },
                                font: function (context) {
                                    if (context.tick && context.tick.major) {
                                        return {
                                            weight: 'bold',
                                        };
                                    }
                                }
                            }
                        }
                    },
                    animation,
                    interaction: {
                        intersect: false
                    },
                    elements: {
                        line: {
                            tension: 0.4,
                        }
                    }
                }
            });
        }
    }));
}

function reload_chart_2() {
    $.getJSON(`/history/sum_category?user_id=${id}&start_date=${start_.format('DD.MM.YYYY')}&end_date=${end_.format('DD.MM.YYYY')}&del=${seeDeleted_.toString()}&category_id=${cat_id_}`, (res => {

        const data = res.map(expense => {return expense.price});
        const labels = res.map(expense => {return expense.category_name;});

        console.log(labels);

        if (chart_2_main !== undefined) {
            chart_2_main.data.labels = labels;
            chart_2_main.data.datasets[0].data = data;
            chart_2_main.update();
        }
        else {
            chart_2_main = new Chart(chart_2, {
                type: 'doughnut',
                data: {
                    labels: labels,
                    datasets: [{
                        data: data,
                        title: 'Sum of Expenses per category',
                        backgroundColor: [
                            'rgb(255, 99, 132)',
                            'rgb(54, 162, 235)',
                            'rgb(255, 205, 86)'
                        ],
                        hoverOffset: 4,
                    }],
                },
            });
        }
    }));
}



$(function(){
    chart_1 = $('#first_chart');
    chart_2 = $('#second_chart');

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
        reload_chart_1();
        reload_chart_2();
    });
    start_ = moment().startOf('month')
    end_ = moment().endOf('month')

    $("#showDeleted").change(function() {
        seeDeleted_ = $("#showDeleted").is(':checked');
        reload_chart_1();
        reload_chart_2();
    });

    $("input[name=category]").change(function() {
        cat_id_ = $(this).val();
        reload_chart_1();
        reload_chart_2();
    });

    $('.sub-category-select').hide();

    $('#select-category').on('change', function() {
        $('.sub-category-select').hide();
        if ($(this).val() === '-1') {
            $('#category_id_input').val(-1);
        } else {
            $('#category_id_input').val($(`#select-${$(this).val()}`).val());
        }
        cat_id_ = $('#category_id_input').val();
        $(`#select-${$(this).val()}`).show();
        reload_chart_1();
        reload_chart_2();
    });

    $('.sub-category-select').on('change', function() {
        cat_id_ = $(this).val();
        $('#category_id_input').val(cat_id_);
        reload_chart_1();
        reload_chart_2();
    });


    reload_chart_1();
    reload_chart_2();
});
