import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { IonicModule, NavParams } from '@ionic/angular';

@Component({
  selector: 'app-define-expense',
  templateUrl: './define-expense.component.html',
  styleUrls: ['./define-expense.component.scss'],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    IonicModule,
  ],
})
export class DefineExpenseComponent  implements OnInit {
  readonly loading = signal(false);
  navProps = inject(NavParams).data;
  constructor() { }

  ngOnInit() {
    console.log(this.navProps);
  }

}
