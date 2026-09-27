import { post } from "./client";
export const aiApi = {
  image: (file) => {
    const data = new FormData();
    data.append("image", file);
    return post("/ai/image/to/ingredient", data);
  },
  addImage: (d) => post("/ai/image/to/ingredient/add", d),
  substitute: (type, recipe, ingredient) =>
    post(`/ai/ingredient/substitute/${recipe}/${type}/${ingredient}`),
  recommend: (request) => post("/ai/recipe/recommendation", { request }),
  rescue: (items) => post("/ai/leftover/rescue", items),
  generate: (request) => post("/ai/recipe/generator", { request }),
  save: (d) => post("/ai/recipe/generator/add", d),
};
