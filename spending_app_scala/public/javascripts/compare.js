let start_;
let end_;
let id;
let cat_id_ = -1;

let chart;
let chart_main;

function reload() {
    $.getJSON(`/compare/data?user_id=${id}&start_date=${start_.format('DD.MM.YYYY')}&end_date=${end_.format('DD.MM.YYYY')}&category_id=${cat_id_}`, (res => {
        $('tr.inside-tr').remove();
        for (const month_sum of res) {
            $('#table-diff tbody').append(
                `<tr class = "inside-tr" >
                    <td class="table__name text-center">${month_sum.year}</td>
                    <td class="table__name text-center">${month_sum.month}</td>
                    <td class="table__name text-center">${month_sum.difference}%</td>
                    <td class="table__name text-center">${month_sum.sum}</td>
                </tr>`
            );
        }

        reload_chart(res);
    }));
}

function reload_chart(res){
    const data = res.map(expense => {
        return {
            y: expense.difference,
            x: new Date(expense.month + '/01/' + expense.year),
            labelTitle: `${expense.month}/${expense.year}`,
            tooltipValue: [`Sum of expenses: ${expense.sum}`,`Difference from first month: ${expense.difference}`]
        }
    });

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
    if (chart_main !== undefined) {
        chart_main.data.datasets[0].data = data;
        chart_main.update();
    }
    else {
        chart_main = new Chart(chart, {
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
                    },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                return context.raw.tooltipValue;
                            },
                            title: function(context) {
                                return context[0].raw.labelTitle;
                            }
                        }
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
            }
        });
    }
}

$(function(){
    chart = $('#first_chart');

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
        startDate: moment().startOf('year'),
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
    start_ = moment().startOf('year')
    end_ = moment().endOf('month')

    $("input[name=category]").change(function() {
        cat_id_ = $(this).val();
        reload();
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
        reload();
    });

    $('.sub-category-select').on('change', function() {
        cat_id_ = $(this).val();
        $('#category_id_input').val(cat_id_);
        reload();
    });

    reload();
});
