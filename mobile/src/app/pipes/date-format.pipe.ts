import { Pipe, PipeTransform } from '@angular/core';

type AllowedDateParts = 'day'|'month'|'year'|'time';

@Pipe({
  name: 'dateFormat',
  standalone: true
})
export class DateFormatPipe implements PipeTransform {
  private getShortMonthName(monthIndex: number): string {
    const monthNames = [
      'JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN',
      'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'
    ];
    return monthNames[monthIndex];
  }
  transform(dateStr: string, ...args: AllowedDateParts[]): string {
    if (!dateStr) {
      return '';
    }
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) {
      return '';
    }
    const formattedTime = date.toLocaleTimeString('en-US', {
      hour: 'numeric', // 'numeric' will output 1-12 for AM/PM, or 0-23 for 24-hour if hour12 is false
      minute: '2-digit', // '2-digit' ensures 0-padded minutes (e.g., 05 instead of 5)
      hour12: true // true for AM/PM format
    });
    const dateMap = {
      time: formattedTime.split(' ').join(''),
      day:  date.getDate(),
      month:  this.getShortMonthName(date.getMonth()),
      year:  date.getFullYear(),
    }
    const finalDateParts: (string|number)[] = [];
    args.forEach((arg) => {
      finalDateParts.push(dateMap[arg])
    })
    return finalDateParts.join(' ');
  }
}
