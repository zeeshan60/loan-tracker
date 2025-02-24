import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'shortenName',
  standalone: true
})
export class ShortenNamePipe implements PipeTransform {

  transform(name: string, ...args: unknown[]): unknown {
    const parts = name.trim().split(" ");
    if (parts.length === 1) return name;
    return `${parts[0]} ${parts[1][0]}`;
  }

}
