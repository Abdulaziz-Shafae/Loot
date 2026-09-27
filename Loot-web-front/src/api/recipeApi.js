import { get, post, put, del } from "./client";
const base = (type) => (type === "system" ? "system" : "recipe");
export const recipeApi = {
  list: (type) => get(`/${base(type)}/get`),
  availability: (type) => get(`/user/availability/${type}`),
  detail: (type, id) => get(`/user/cook/${type}/${id}`),
  ingredients: (type) => get(`/${base(type)}/ingredient/get`),
  add: (type, d) => post(`/${base(type)}/add`, d),
  edit: (type, id, d) => put(`/${base(type)}/update/${id}`, d),
  remove: (type, id) => del(`/${base(type)}/delete/${id}`),
  addIngredient: (type, d) => post(`/${base(type)}/ingredient/add`, d),
  editIngredient: (type, id, d) =>
    put(`/${base(type)}/ingredient/update/${id}`, d),
  removeIngredient: (type, id) => del(`/${base(type)}/ingredient/delete/${id}`),
  cook: (type, id) => post(`/user/cook/${type}/${id}/done`),
  convert: (id) => post(`/user/system/${id}/convert`),
};
