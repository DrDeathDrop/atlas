import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'enumLabel' })
export class EnumLabelPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) {
      return '';
    }
    const words = value.toLowerCase().split('_').join(' ');
    return words.charAt(0).toUpperCase() + words.slice(1);
  }
}
