import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Textarea from 'primevue/textarea'
import Select from 'primevue/select'
import DatePicker from 'primevue/datepicker'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Dialog from 'primevue/dialog'
import Tag from 'primevue/tag'
import Toast from 'primevue/toast'
import ToggleSwitch from 'primevue/toggleswitch'
import Drawer from 'primevue/drawer'
import Password from 'primevue/password'
import Paginator from 'primevue/paginator'

export function registrarComponentes(app) {
  Object.entries({ Button, InputText, InputNumber, Textarea, Select, DatePicker, DataTable, Column, Dialog, Tag, Toast,
    ToggleSwitch, Drawer, Password, Paginator }).forEach(([n, c]) => app.component(n, c))
}
