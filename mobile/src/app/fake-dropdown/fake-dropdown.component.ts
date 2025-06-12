import { ChangeDetectionStrategy, Component, forwardRef, input } from '@angular/core';
import { IonIcon } from '@ionic/angular/standalone';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
  selector: 'mr-fake-dropdown',
  templateUrl: './fake-dropdown.component.html',
  styleUrls: ['./fake-dropdown.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    IonIcon,
  ],
  providers: [{ provide: NG_VALUE_ACCESSOR, useValue: forwardRef(() => FakeDropdownComponent)}],
})
export class FakeDropdownComponent implements ControlValueAccessor{
  label = input.required<string>();
  selectedText = input.required<string>();
  constructor() {
  }

  writeValue(obj: any): void {
    throw new Error('Method not implemented.');
  }
  registerOnChange(fn: any): void {
    throw new Error('Method not implemented.');
  }
  registerOnTouched(fn: any): void {
    throw new Error('Method not implemented.');
  }
  setDisabledState?(isDisabled: boolean): void {
    throw new Error('Method not implemented.');
  }
}
