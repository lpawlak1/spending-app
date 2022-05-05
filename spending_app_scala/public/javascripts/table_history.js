let start_;
let end_;
let id;
let seeDeleted_ = false;
let cat_id_ = -1;

function table_button_click(method) {
    return function() {
        const id = $(this).parent().find('.hidden').text();

        const token = $('#CSRF input').val();

        let del;
        del = method === 'DELETE';

        $.ajax( `/expense/${id}?del=${del}`, {
            type : method,
            data : {
                CSRF_token : token
            },
            headers: {
                'Csrf-Token': token
            },
        })
            .then((res) => {
                reload();
            })
            .catch(err => {
                console.log(err);
            });
    }
}

function reload() {
    $.getJSON(`/history/get_row?user_id=${id}&start_date=${start_.format('DD.MM.YYYY')}&end_date=${end_.format('DD.MM.YYYY')}&del=${seeDeleted_.toString()}&category_id=${cat_id_}`, (res => {
        $('tr.inside-tr').remove();
        for (const expense of res) {
            let button;
            if (expense.deleted === 'false') {
                button = `<td class="table__remove"><button><i class= "fa-solid fa-xmark red_mark"> </i></button></td>`
            }
            else {
                button = `<td class="table__readd"><button><i class="fa-solid fa-check green_mark"></i></button></td>`
            }
            console.log(`<tr class="inside-tr">
                <td class="hidden">${expense.id}</td>
                <td class="table__name">${expense.name}</td>
                <td> ${expense.category}</td>
                <td class="table__date">${expense.purchase_date}</td>
                <td class="table__price">${expense.price}</td>
                <td class="table__description">${expense.desc}</td>
                ${button}
            </tr>`);

            $('.table__history tbody').append(
                `<tr class="inside-tr">
                    <td class="hidden">${expense.id}</td>
                    <td class="table__name">${expense.name}</td>
                    <td> ${expense.category}</td>
                    <td class="table__date">${expense.purchase_date}</td>
                    <td class="table__price">${expense.price}</td>
                    <td class="table__description">${expense.desc}</td>
                    ${button}
                </tr>`
            );
        }
        $('.table__remove').click(table_button_click('DELETE'));
        $('.table__readd').click(table_button_click('PUT'));
    }));
}

$(function(){
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
