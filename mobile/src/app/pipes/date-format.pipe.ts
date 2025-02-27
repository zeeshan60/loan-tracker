import { Pipe, PipeTransform } from '@angular/core';

type AllowedDateParts = 'day'|'month'|'year';

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
    const dateMap = {
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
