import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { IonIcon } from '@ionic/angular/standalone';

@Component({
  selector: 'mr-fake-dropdown',
  templateUrl: './fake-dropdown.component.html',
  styleUrls: ['./fake-dropdown.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    IonIcon,
  ],
})
export class FakeDropdownComponent {
  label = input.required<string>();
  selectedText = input.required<string>();
  constructor() {
  }
}
